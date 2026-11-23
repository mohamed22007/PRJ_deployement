variable "region" {

  type    = string
  default = "eu-west-3"

}

variable "instance_type" {

  type    = string
  default = "t3.micro"

}

variable "my_ip_cidr" {
  description = "Adrres Ip pour la connection ssh "
  type        = string
}

variable "public_key_path" {
  type    = string
  default = "~/.ssh/prj_deploy.pub"
}