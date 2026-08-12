package com.ccadmin.app.sale.model.entity;

import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "sale_delivery")
public class SaleDeliveryEntity extends AuditTableEntity implements Serializable {

    @Id
    public String SaleCod;
    public String DeliveryTypeCod;
    public String DeliveryStatus = SaleConstants.DELIVERY_STATUS_PENDING;
    public Long ClientAddressID;
    public String IsThirdParty = "N";
    public String Names;
    public String DocumentType;
    public String DocumentNumber;
    public String Phone;
    public String Email;
    public String Address;
    public String GeocodedAddress;
    public String Reference;
    public String CountryCod;
    public String CountryName;
    public String StateName;
    public String CityName;
    public String UbigeoCod;
    public BigDecimal Latitude;
    public BigDecimal Longitude;
    public String Instructions;
    public BigDecimal EstimatedDistanceKm;
    public Date ScheduledFrom;
    public Date ScheduledTo;
    public String ShippingProviderCod;
    public String TrackingNumber;
    public String AgencyName;
    public String AgencyAddress;
    public Date ReadyDate;
    public Date DispatchDate;
    public Date DeliveredDate;
    public String Commenter;

    public SaleDeliveryEntity() {
    }
}
