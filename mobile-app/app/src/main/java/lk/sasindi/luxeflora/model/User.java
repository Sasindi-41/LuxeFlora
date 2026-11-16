package lk.sasindi.luxeflora.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User {
    private String uid;
    private String name;
    private String email;
    private String profilePicUrl;

    private boolean online;
    private long lastSeen;

    private String contactNo;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String postalCode;
}