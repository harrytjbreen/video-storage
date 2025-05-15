# Data block to reference the existing Route53 hosted zone
data "aws_route53_zone" "primary" {
  name = "harrybreen.co.uk."
}

# Route53 record to point video.harrybreen.co.uk to the CloudFront distribution for the frontend
resource "aws_route53_record" "video_frontend_alias" {
  zone_id = data.aws_route53_zone.primary.zone_id
  name    = "video.harrybreen.co.uk"
  type    = "A"

  alias {
    name                   = aws_cloudfront_distribution.frontend_distribution.domain_name
    zone_id                = aws_cloudfront_distribution.frontend_distribution.hosted_zone_id
    evaluate_target_health = false
  }
}