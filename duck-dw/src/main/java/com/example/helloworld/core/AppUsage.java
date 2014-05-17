package com.example.helloworld.core;

import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name = "appusage" )
@NamedQueries({
    @NamedQuery(
            name = "com.example.helloworld.core.AppUsage.findAll",
            query = "SELECT au FROM AppUsage au"
    ),
    @NamedQuery(
            name = "com.example.helloworld.core.AppUsage.findById",
            query = "SELECT au FROM AppUsage au where au.id = :id"
    )
})

@Data
public class AppUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "appName", nullable = false)
    private String appName;

    @Column(name = "signature", nullable = false)
    private String signature;

    @Column(name = "usedAtMillis", nullable = false)
    private long usedAtMillis;

}
