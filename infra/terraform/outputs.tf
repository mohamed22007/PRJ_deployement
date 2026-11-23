output "public_ip" {
  value = aws_instance.app.public_ip
}

output "ssh_command" {
  value = "ssh -i ~/.ssh/prj_deploy ubuntu@${aws_instance.app.public_ip}"
}