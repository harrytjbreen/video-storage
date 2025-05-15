resource "aws_cloudfront_distribution" "video_app_distribution" {
  enabled = true

  origin {
    domain_name = aws_s3_bucket.video_bucket.bucket_regional_domain_name
    origin_id   = "videoS3Origin"

    s3_origin_config {
      origin_access_identity = aws_cloudfront_origin_access_identity.video_oai.cloudfront_access_identity_path
    }
  }

  default_cache_behavior {
    allowed_methods  = ["GET", "HEAD"]
    cached_methods   = ["GET", "HEAD"]
    target_origin_id = "videoS3Origin"

    forwarded_values {
      query_string = false

      cookies {
        forward = "none"
      }
    }

    viewer_protocol_policy = "redirect-to-https"
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    cloudfront_default_certificate = true
  }

  default_root_object = "index.html"
}

resource "aws_cloudfront_origin_access_identity" "video_oai" {
  comment = "OAI for video app S3 bucket"
}

