package com.example.geoserver.entity

import jakarta.persistence.*

@Entity
@Table(name = "report_templates")
class ReportTemplate(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var name: String,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(columnDefinition = "TEXT", nullable = false)
    var jrxmlContent: String,

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    var metadataJson: String? = null
)