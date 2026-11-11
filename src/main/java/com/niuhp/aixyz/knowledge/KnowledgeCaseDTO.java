package com.niuhp.aixyz.knowledge;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 第五篇知识库接口数据，只保存显式字段。
 *
 * @author niuhaipeng
 */
public class KnowledgeCaseDTO {

    @NotBlank
    @Size(max = 800)
    private String id;

    @NotBlank
    @Size(max = 800)
    private String title;

    @NotBlank
    @Size(max = 800)
    private String system;

    @NotBlank
    @Size(max = 800)
    private String module;

    @NotBlank
    @Size(max = 800)
    private String symptoms;

    @NotBlank
    @Size(max = 800)
    private String resolution;

    @NotBlank
    @Size(max = 800)
    private String applicability;

    @NotBlank
    @Size(max = 800)
    private String version;

    @NotBlank
    @Size(max = 800)
    private String updatedAt;

    @NotNull
    private Boolean teachingOnly;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSystem() {
        return system;
    }

    public void setSystem(String system) {
        this.system = system;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getSymptoms() {
        return symptoms;
    }

    public void setSymptoms(String symptoms) {
        this.symptoms = symptoms;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public String getApplicability() {
        return applicability;
    }

    public void setApplicability(String applicability) {
        this.applicability = applicability;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Boolean getTeachingOnly() {
        return teachingOnly;
    }

    public void setTeachingOnly(Boolean teachingOnly) {
        this.teachingOnly = teachingOnly;
    }

    @Override
    public String toString() {
        return "KnowledgeCaseDTO{content=[REDACTED]}";
    }
}
