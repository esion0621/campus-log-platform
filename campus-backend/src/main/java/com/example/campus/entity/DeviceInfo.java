package com.example.campus.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "device_info")
@Data
public class DeviceInfo {
    @Id
    @Column(name = "device_id", length = 20)
    private String deviceId;

    @Column(name = "device_name", length = 50)
    private String deviceName;

    @Column(name = "device_type", length = 10)
    private String deviceType;

    @Column(name = "location", length = 100)
    private String location;
}
