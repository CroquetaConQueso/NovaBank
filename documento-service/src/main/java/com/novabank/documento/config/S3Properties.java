package com.novabank.documento.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "novabank")
public class S3Properties {

    private Aws aws = new Aws();
    private S3 s3 = new S3();

    public Aws getAws() {
        return aws;
    }

    public void setAws(Aws aws) {
        this.aws = aws;
    }

    public S3 getS3() {
        return s3;
    }

    public void setS3(S3 s3) {
        this.s3 = s3;
    }

    public static class Aws {
        private String region = "eu-west-1";
        private String endpointOverride;

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getEndpointOverride() {
            return endpointOverride;
        }

        public void setEndpointOverride(String endpointOverride) {
            this.endpointOverride = endpointOverride;
        }
    }

    public static class S3 {
        private String bucketJustificantes = "novabank-justificantes";
        private Duration presignedUrlTtl = Duration.ofMinutes(15);

        public String getBucketJustificantes() {
            return bucketJustificantes;
        }

        public void setBucketJustificantes(String bucketJustificantes) {
            this.bucketJustificantes = bucketJustificantes;
        }

        public Duration getPresignedUrlTtl() {
            return presignedUrlTtl;
        }

        public void setPresignedUrlTtl(Duration presignedUrlTtl) {
            this.presignedUrlTtl = presignedUrlTtl;
        }
    }
}
