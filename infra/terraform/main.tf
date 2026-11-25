#Image ubuntu le plus récente
data "aws_ami" "ubuntu" {
  most_recent = true
  owners      = ["099720109477"]
  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd-gp3/ubuntu-noble-24.04-amd64-server-*"]
  }
}

resource "aws_key_pair" "deploy" {

  key_name   = "prj-deploy"
  public_key = file(pathexpand(var.public_key_path))

}

resource "aws_security_group" "web" {
  name        = "prj-web"
  description = "SSh pour mon IP et HTTP/HTTPS public "

  ingress {
    description = "ssh"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = [var.my_ip_cidr] #Actuelment ssh est possible pour que github peut dployer les machine
  }
  #checkov:skip=CKV_AWS_260: Site web public accecible par tout
  ingress {
    description = "HTTP"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  ingress {
    description = "HTTPS"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  egress {
    description = "Pour les requets HTTPS docker pull et apt "
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  egress {
    description = "Pour les requets HTTP sortant"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_instance" "app" {
  ami                    = data.aws_ami.ubuntu.id
  instance_type          = var.instance_type
  key_name               = aws_key_pair.deploy.key_name
  vpc_security_group_ids = [aws_security_group.web.id]
  ebs_optimized = true
  #checkov:skip=CKV_AWS_126:Monitoring 5 min suffisant pour ce projet, le detailed monitoring est payant
  #monitoring = true
  iam_instance_profile = aws_iam_instance_profile.ec2.name

  root_block_device {
    volume_size = 20
    volume_type = "gp3"
    encrypted   = true
  }

  # Pour eviter les failles SSRF
  metadata_options {
    http_tokens = "required"
  }

  lifecycle {
    ignore_changes = [ami]   # une nouvelle image Ubuntu ne doit jamais recréer le serveur
  }

  tags = { Name = "prj_dhamet" }
}

resource "aws_eip" "app" {
  instance = aws_instance.app.id
  domain   = "vpc"
  tags     = { Name = "prj_dhamet" }
}


