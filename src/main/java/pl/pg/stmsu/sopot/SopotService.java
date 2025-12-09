package pl.pg.stmsu.sopot;

import lombok.Getter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import pl.pg.stmsu.sopot.ws.Coordinates;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

@Service
public class SopotService {
    private final float mapWidth = 2597; // meters
    private final float mapHeight = 2587; // meters
    private final float mapDiagonal = 3665; // meters
    private final float topLatitude = 54.457306F; // north
    private final float leftLongitude = 18.548444F; // west
    private final float bottomLatitude = 54.434033F; // south
    private final float rightLongitude = 18.588344F; // east

    @Getter
    private final BufferedImage map;

    public SopotService() throws IOException {
        map = ImageIO.read(new ClassPathResource("map.png").getInputStream());
    }

    public BufferedImage cutFragment(int x1, int y1, int x2, int y2) {
        int width = x2 - x1;
        int height = y2 - y1;
        return map.getSubimage(x1, y1, width, height);
    }

    public String toBase64(BufferedImage img) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    public String getMapFragment(int x1, int y1, int x2, int y2) {
        try {
            int left   = Math.min(x1, x2);
            int top    = Math.min(y1, y2);
            int right  = Math.max(x1, x2);
            int bottom = Math.max(y1, y2);
            BufferedImage fragment = this.cutFragment(left, top, right, bottom);
            return this.toBase64(fragment);
        } catch (Exception e) {
            throw new RuntimeException("Cannot cut map fragment", e);
        }
    }

    public float pixelXToLongitude(int x) {
        return leftLongitude + (x / 1000.0f) * (rightLongitude - leftLongitude);
    }

    public float pixelYToLatitude(int y) {
        return topLatitude - (y / 1000.0f) * (topLatitude - bottomLatitude);
    }

    public int longitudeToPixelX(float lon) {
        return (int) ((lon - leftLongitude) / (rightLongitude - leftLongitude) * 1000.0);
    }

    public int latitudeToPixelY(float lat) {
        return (int) ((topLatitude - lat) / (topLatitude - bottomLatitude) * 1000.0);
    }

    public float parseCoordinate(String raw) {
        if (raw == null || raw.isBlank())
            throw new IllegalArgumentException("Empty coordinate");
        raw = raw.trim();
        if (raw.endsWith("px")) {
            String num = raw.substring(0, raw.length() - 2);
            return Integer.parseInt(num);
        }
        if (raw.contains("°")) {
            return dmsToDecimal(raw);
        }
        try {
            return Float.parseFloat(raw);
        } catch (NumberFormatException ignore) {}
        throw new IllegalArgumentException("Unsupported format: " + raw);
    }

    public static float dmsToDecimal(String dms) {
        if (dms == null || dms.isBlank())
            throw new IllegalArgumentException("Empty DMS");
        dms = dms.trim();
        int sign = 1;
        if (dms.matches(".*[SWsw]$")) {
            sign = -1;
        }
        dms = dms.replaceAll("(?i)[NSEW]$", "").trim();
        dms = dms.replace("°", " ")
            .replace("'", " ")
            .replace("’", " ")
            .replace("′", " ")
            .replace("\"", " ")
            .replace("”", " ")
            .replace("″", " ")
            .replaceAll("\\s+", " ");
        String[] parts = dms.split(" ");
        float deg = 0, min = 0, sec = 0;
        if (parts.length > 0 && !parts[0].isEmpty()) deg = Float.parseFloat(parts[0]);
        if (parts.length > 1 && !parts[1].isEmpty()) min = Float.parseFloat(parts[1]);
        if (parts.length > 2 && !parts[2].isEmpty()) sec = Float.parseFloat(parts[2]);
        return sign * (deg + min / 60f + sec / 3600f);
    }

    public static String decimalToDms(float decimal, boolean isLatitude) {
        char hemisphere = isLatitude
            ? (decimal >= 0 ? 'N' : 'S')
            : (decimal >= 0 ? 'E' : 'W');
        float abs = Math.abs(decimal);
        int degrees = (int) abs;
        float minutesFull = (abs - degrees) * 60f;
        int minutes = (int) minutesFull;
        float seconds = (minutesFull - minutes) * 60f;
        return String.format(
            java.util.Locale.US,
            "%d°%d'%.3f\" %c",
            degrees, minutes, seconds, hemisphere
        );
    }
    private Object parseValue(String input) {
        if (input == null) return null;
        String s = input.trim();
        if (s.matches("^-?\\d+$")) {
            return Integer.parseInt(s);
        }
        if (s.matches("^-?\\d+\\.\\d+$")) {
            return Float.parseFloat(s);
        }
        return s;
    }

    public Coordinates getCoordinates(String rawX, String rawY){
        Object x = parseValue(rawX);
        Object y = parseValue(rawY);
        Coordinates newCoordinates = new Coordinates();
        if(x instanceof Integer) {
            newCoordinates.setX((Integer) x);
            newCoordinates.setLon(pixelXToLongitude((Integer) x));
            newCoordinates.setLongitude(decimalToDms(pixelXToLongitude((Integer) x),false));
        }
        if(y instanceof Integer) {
            newCoordinates.setY((Integer) y);
            newCoordinates.setLat(pixelYToLatitude((Integer) y));
            newCoordinates.setLatitude(decimalToDms(pixelYToLatitude((Integer) y),true));
        }
        if(x instanceof Float) {
            newCoordinates.setX(longitudeToPixelX((Float) x));
            newCoordinates.setLon((Float) x);
            newCoordinates.setLongitude(decimalToDms((Float) x,false));
        }
        if(y instanceof Float) {
            newCoordinates.setY(latitudeToPixelY((Float) y));
            newCoordinates.setLat((Float) y);
            newCoordinates.setLatitude(decimalToDms((Float) y,true));
        }
        if(x instanceof String) {
            newCoordinates.setX(longitudeToPixelX(dmsToDecimal((String) x)));
            newCoordinates.setLon(dmsToDecimal((String) x));
            newCoordinates.setLongitude((String) x);
        }
        if(y instanceof String) {
            newCoordinates.setY(latitudeToPixelY(dmsToDecimal((String) y)));
            newCoordinates.setLat(dmsToDecimal((String) y));
            newCoordinates.setLatitude((String) y);
        }
        return newCoordinates;
    }
}
