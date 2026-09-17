package com.brandx.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Size;

/**
 * Postal address, embedded into {@link Customer} rather than given its own table.
 * Kept as an {@code @Embeddable} so it can be reused (e.g. a future shipping
 * address on {@link Order}) without a schema migration.
 */
@Embeddable
public class Address {

    @Size(max = 200)
    @Column(name = "address_line1", length = 200)
    private String line1;

    @Size(max = 200)
    @Column(name = "address_line2", length = 200)
    private String line2;

    @Size(max = 100)
    @Column(name = "address_city", length = 100)
    private String city;

    @Size(max = 100)
    @Column(name = "address_state", length = 100)
    private String state;

    @Size(max = 20)
    @Column(name = "address_postal_code", length = 20)
    private String postalCode;

    @Size(max = 100)
    @Column(name = "address_country", length = 100)
    private String country;

    public String getLine1() {
        return line1;
    }

    public void setLine1(String line1) {
        this.line1 = line1;
    }

    public String getLine2() {
        return line2;
    }

    public void setLine2(String line2) {
        this.line2 = line2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    /** True when every field is blank, so views can skip rendering an empty block. */
    public boolean isEmpty() {
        return isBlank(line1) && isBlank(line2) && isBlank(city)
                && isBlank(state) && isBlank(postalCode) && isBlank(country);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
