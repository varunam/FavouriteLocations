package app.favloc.com.favouritelocations;

/**
 * Created by vaam on 19-05-2017.
 */

public class UserData {
    private String firstName, lastName, dateofbirth, locality, gender, email;

    public UserData()
    {

    }

    public UserData(String firstName, String lastName, String dateofbirth, String locality, String gender, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateofbirth = dateofbirth;
        this.locality = locality;
        this.gender = gender;
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getDateofbirth() {
        return dateofbirth;
    }

    public String getLocality() {
        return locality;
    }

    public String getGender() {
        return gender;
    }

    public String getEmail() {
        return email;
    }
}
