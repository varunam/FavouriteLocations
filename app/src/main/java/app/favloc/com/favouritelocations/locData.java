package app.favloc.com.favouritelocations;

/**
 * Created by vaam on 24-05-2017.
 */

public class locData {
    public String locName;
    public String locLandMark;
    public String locLat;
    public String locLng;
    public String refKey;

    public String getRefKey() {
        return refKey;
    }

    locData()
    {

    }

    public locData(String locName, String locLandMark, String locLat, String locLng, String referenceKey) {
        this.locName = locName;
        this.locLandMark = locLandMark;
        this.locLat = locLat;
        this.locLng = locLng;
        this.refKey = referenceKey;
    }

    public String getLocName() {
        return locName;
    }

    public String getLocLandMark() {
        return locLandMark;
    }

    public String getLocLat() {
        return locLat;
    }

    public String getLocLng() {
        return locLng;
    }
}
