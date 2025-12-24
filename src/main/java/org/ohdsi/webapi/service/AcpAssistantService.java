package org.ohdsi.webapi.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.ohdsi.circe.vocabulary.ConceptSetExpression;
import org.ohdsi.webapi.conceptset.ConceptSet;
import org.ohdsi.webapi.conceptset.ConceptSetItem;
import org.ohdsi.webapi.conceptset.ConceptSetRepository;
import org.ohdsi.webapi.service.dto.ConceptSetDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@Path("/ohdsi_acp/")
@Produces(MediaType.APPLICATION_JSON)
public class AcpAssistantService extends AbstractDaoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AcpAssistantService.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final ConceptSetService conceptSetService;
    private final ConceptSetRepository conceptSetRepository;
    private final ObjectMapper mapper = new ObjectMapper().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    private final Map<String, PreparedSession> preparedStore = new ConcurrentHashMap<>();

    @Value("${acp.bridge.url:http://localhost:7777}")
    private String acpBaseUrl;

    public AcpAssistantService(ConceptSetService conceptSetService, ConceptSetRepository conceptSetRepository) {
        this.conceptSetService = conceptSetService;
        this.conceptSetRepository = conceptSetRepository;
    }

    @POST
    @Path("assist")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response assist(AcpAssistRequest request) {
        if (request == null || request.artifact == null || request.task == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Collections.singletonMap("error", "task, study_intent, artifact are required")).build();
        }
        if (!"concept-sets-review".equals(request.task)) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Collections.singletonMap("error", "Only concept-sets-review is supported in this prototype")).build();
        }
        if (request.artifact.id == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Collections.singletonMap("error", "artifact.id is required")).build();
        }

        ConceptSet conceptSet = conceptSetRepository.findById(request.artifact.id);
        if (conceptSet == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(Collections.singletonMap("error", "Concept set not found")).build();
        }

        ConceptSetExpression expression = conceptSetService.getConceptSetExpression(request.artifact.id);
        String checksum = checksum(expression);

        Map<String, Object> artifactPayload = new LinkedHashMap<>();
        artifactPayload.put("type", "conceptSet");
        artifactPayload.put("id", request.artifact.id);
        artifactPayload.put("name", conceptSet.getName());
        artifactPayload.put("expression", expression);

        Map<String, Object> acpPayload = new LinkedHashMap<>();
        acpPayload.put("caller", "WebAPI");
        acpPayload.put("task", request.task);
        acpPayload.put("study_intent", request.studyIntent == null ? "" : request.studyIntent);
        acpPayload.put("artifact", artifactPayload);
        acpPayload.put("capabilities", Collections.singletonList("prepare_webapi_mutations"));
        Map<String, Object> preimage = new LinkedHashMap<>();
        preimage.put("conceptSetId", request.artifact.id);
        preimage.put("checksum", checksum);
        acpPayload.put("preimage", preimage);

        Map<String, Object> acpResponse;
        try {
            acpResponse = restTemplate.postForObject(acpBaseUrl + "/assist/analyze", acpPayload, Map.class);
        } catch (RestClientException e) {
            LOGGER.warn("acp-assist call failed: {}", e.getMessage());
            return Response.status(Response.Status.BAD_GATEWAY).entity(Collections.singletonMap("error", "ACP bridge unavailable")).build();
        }
        if (acpResponse == null) {
            return Response.status(Response.Status.BAD_GATEWAY).entity(Collections.singletonMap("error", "Empty response from ACP")).build();
        }

        List<Map<String, Object>> preparedMutations = castMutations(acpResponse.get("prepared_mutations"));
        String nonce = UUID.randomUUID().toString();
        PreparedSession session = new PreparedSession();
        session.user = security.getSubject();
        session.conceptSetId = request.artifact.id;
        session.preimageChecksum = checksum;
        session.mutations = preparedMutations;
        preparedStore.put(nonce, session);

        List<Map<String, Object>> mutationPreviews = preparedMutations.stream().map(m -> {
            Map<String, Object> preview = new LinkedHashMap<>();
            preview.put("label", m.get("label"));
            preview.put("preimage", m.get("preimage"));
            preview.put("preview_changes", m.get("preview_changes"));
            preview.put("apply_policy", m.get("apply_policy"));
            return preview;
        }).collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("plan", acpResponse.get("plan"));
        response.put("findings", acpResponse.get("findings"));
        response.put("actions", acpResponse.get("actions"));
        response.put("prepared_mutations", mutationPreviews);
        response.put("nonce", nonce);
        Map<String, Object> responsePreimage = new LinkedHashMap<>();
        responsePreimage.put("conceptSetId", request.artifact.id);
        responsePreimage.put("checksum", checksum);
        response.put("preimage", responsePreimage);
        Map<String, Object> artifactSummary = new LinkedHashMap<>();
        artifactSummary.put("id", request.artifact.id);
        artifactSummary.put("name", conceptSet.getName());
        response.put("artifact", artifactSummary);
        LOGGER.info("acp-assist task={} conceptSetId={} nonce={} user={}", request.task, request.artifact.id, nonce, session.user);
        return Response.ok(response).build();
    }

    @POST
    @Path("apply")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response apply(AcpApplyRequest request) {
        if (request == null || request.nonce == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Collections.singletonMap("error", "nonce is required")).build();
        }
        PreparedSession session = preparedStore.get(request.nonce);
        if (session == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(Collections.singletonMap("error", "No prepared mutations found for nonce")).build();
        }
        if (session.user != null && !session.user.equals(security.getSubject())) {
            return Response.status(Response.Status.FORBIDDEN).entity(Collections.singletonMap("error", "Requestor does not match preview owner")).build();
        }

        ConceptSetExpression liveExpression = conceptSetService.getConceptSetExpression(session.conceptSetId);
        String currentChecksum = checksum(liveExpression);
        if (session.preimageChecksum != null && !session.preimageChecksum.equals(currentChecksum)) {
            return Response.status(Response.Status.CONFLICT).entity(Collections.singletonMap("error", "Concept set has changed since preview")).build();
        }
        if (request.preimage != null && request.preimage.containsKey("conceptSetId")) {
            Object cid = request.preimage.get("conceptSetId");
            if (cid != null && !cid.toString().equals(String.valueOf(session.conceptSetId))) {
                return Response.status(Response.Status.CONFLICT).entity(Collections.singletonMap("error", "Concept set reference mismatch")).build();
            }
        }

        Map<String, Object> mutation = pickMutation(session.mutations, request.preparedMutationLabel);
        if (mutation == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Collections.singletonMap("error", "No prepared mutation available")).build();
        }
        List<Map<String, Object>> requests = castMutations(mutation.get("requests"));
        if (requests.size() < 2) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Collections.singletonMap("error", "Prepared mutation is missing required requests")).build();
        }

        Map<String, Object> createReq = requests.get(0);
        Map<String, Object> createBody = safeMap(createReq.get("body"));
        if (!createBody.containsKey("name")) {
            createBody.put("name", "Assistant concept set");
        }
        Integer newConceptSetId;
        try {
            ConceptSetDTO dto = mapper.convertValue(createBody, ConceptSetDTO.class);
            ConceptSetDTO created = conceptSetService.createConceptSet(dto);
            newConceptSetId = created.getId();

            Map<String, Object> exprReq = requests.get(1);
            ConceptSetExpression expression = mapper.convertValue(exprReq.get("body"), ConceptSetExpression.class);
            conceptSetService.saveConceptSetItems(newConceptSetId, toConceptSetItems(expression));
        } catch (Exception e) {
            LOGGER.warn("acp-apply failed: {}", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Collections.singletonMap("error", "Failed to apply prepared mutation")).build();
        }

        preparedStore.remove(request.nonce);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("newConceptSetId", newConceptSetId);
        response.put("link", String.format("#/conceptset/%d", newConceptSetId));
        response.put("label", mutation.get("label"));
        LOGGER.info("acp-apply baseConceptSetId={} newConceptSetId={} label={} user={}", session.conceptSetId, newConceptSetId, mutation.get("label"), security.getSubject());
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    private List<Map<String, Object>> castMutations(Object raw) {
        if (!(raw instanceof List)) {
            return new ArrayList<>();
        }
        List<?> list = (List<?>) raw;
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map) {
                //noinspection unchecked
                out.add(new LinkedHashMap<>((Map<String, Object>) item));
            }
        }
        return out;
    }

    private Map<String, Object> pickMutation(List<Map<String, Object>> mutations, String label) {
        if (mutations == null || mutations.isEmpty()) {
            return null;
        }
        if (label == null) {
            return mutations.get(0);
        }
        return mutations.stream().filter(m -> label.equals(m.get("label"))).findFirst().orElse(mutations.get(0));
    }

    private ConceptSetItem[] toConceptSetItems(ConceptSetExpression expression) {
        if (expression == null || expression.items == null) {
            return new ConceptSetItem[0];
        }
        List<ConceptSetItem> items = new ArrayList<>();
        for (ConceptSetExpression.ConceptSetItem item : expression.items) {
            if (item == null || item.concept == null) {
                continue;
            }
            ConceptSetItem csi = new ConceptSetItem();
            csi.setConceptId(item.concept.conceptId);
            csi.setIncludeDescendants(Boolean.TRUE.equals(item.includeDescendants) ? 1 : 0);
            csi.setIncludeMapped(Boolean.TRUE.equals(item.includeMapped) ? 1 : 0);
            csi.setIsExcluded(Boolean.TRUE.equals(item.isExcluded) ? 1 : 0);
            items.add(csi);
        }
        return items.toArray(new ConceptSetItem[0]);
    }

    private String checksum(Object obj) {
        try {
            String payload = mapper.writeValueAsString(obj);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashed) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        } catch (Exception e) {
            LOGGER.warn("checksum failed: {}", e.getMessage());
            return UUID.randomUUID().toString();
        }
    }

    private Map<String, Object> safeMap(Object obj) {
        if (obj instanceof Map) {
            //noinspection unchecked
            return new LinkedHashMap<>((Map<String, Object>) obj);
        }
        return new LinkedHashMap<>();
    }

    private static class PreparedSession {
        Integer conceptSetId;
        String preimageChecksum;
        String user;
        List<Map<String, Object>> mutations = new ArrayList<>();
    }

    public static class ArtifactRef {
        public String type;
        public Integer id;
    }

    public static class AcpAssistRequest {
        public String task;
        @JsonProperty("study_intent")
        public String studyIntent;
        public ArtifactRef artifact;
    }

    public static class AcpApplyRequest {
        public String nonce;
        @JsonProperty("prepared_mutation_label")
        public String preparedMutationLabel;
        public Map<String, Object> preimage;
    }
}
