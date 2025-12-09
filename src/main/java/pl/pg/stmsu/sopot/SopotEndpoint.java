package pl.pg.stmsu.sopot;

import lombok.AllArgsConstructor;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import pl.pg.stmsu.sopot.ws.GetCoordinatesRequest;
import pl.pg.stmsu.sopot.ws.GetCoordinatesResponse;
import pl.pg.stmsu.sopot.ws.GetMapFragmentRequest;
import pl.pg.stmsu.sopot.ws.GetMapFragmentResponse;

@Endpoint
@AllArgsConstructor
public class SopotEndpoint {
    private final SopotService sopotService;

    private static final String NAMESPACE_URI = "https://sopot.stmsu.pg.pl/ws";

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "getCoordinatesRequest")
    @ResponsePayload
    public GetCoordinatesResponse getCoordinates(@RequestPayload GetCoordinatesRequest request) {
        GetCoordinatesResponse response = new GetCoordinatesResponse();
        String rawX = request.getX();
        String rawY = request.getY();
        response.setCoordinates(sopotService.getCoordinates(rawX, rawY));
        return response;
    }


    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "getMapFragmentRequest")
    @ResponsePayload
    public GetMapFragmentResponse getMapFragment(@RequestPayload GetMapFragmentRequest req) {
        GetMapFragmentResponse resp = new GetMapFragmentResponse();
        String base64 = sopotService.getMapFragment(
            req.getX1(),
            req.getY1(),
            req.getX2(),
            req.getY2()
        );
        resp.setImageBase64(base64);
        return resp;
    }
}
