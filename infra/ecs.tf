resource "aws_ecs_cluster" "video-download-cluster" {
  name = "video-download-cluster"
}

resource "aws_iam_role" "task_exec_role" {
  name = "video-task-exec-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17",
    Statement = [{
      Effect = "Allow",
      Principal = {
        Service = "ecs-tasks.amazonaws.com"
      },
      Action = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy_attachment" "ecs_task_execution_policy" {
  role       = aws_iam_role.task_exec_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

resource "aws_iam_role_policy" "s3_access" {
  name = "video-s3-access"
  role = aws_iam_role.task_exec_role.id

  policy = jsonencode({
    Version = "2012-10-17",
    Statement = [{
      Action = [
        "s3:GetObject",
        "s3:PutObject",
        "s3:AbortMultipartUpload"
      ],
      Effect   = "Allow",
      Resource = "arn:aws:s3:::your-bucket-name/*"
    }]
  })
}

resource "aws_ecs_task_definition" "video_task" {
  family                   = "video-downloader"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = "512"
  memory                   = "1024"

  execution_role_arn = aws_iam_role.task_exec_role.arn
  task_role_arn      = aws_iam_role.task_exec_role.arn

  container_definitions = jsonencode([
    {
      name      = "video-downloader"
      image     = "your-docker-image-url"
      essential = true
      environment = [
        { name = "VIDEO_URL", value = "https://..." },
        { name = "S3_BUCKET", value = "your-bucket-name" },
        { name = "S3_KEY", value = "videos/output.mp4" }
      ],
      logConfiguration = {
        logDriver = "awslogs",
        options = {
          awslogs-group         = "/ecs/video-download"
          awslogs-region        = "us-east-1"
          awslogs-stream-prefix = "ecs"
        }
      }
    }
  ])
}

resource "aws_cloudwatch_log_group" "ecs_logs" {
  name              = "/ecs/video-download"
  retention_in_days = 3
}