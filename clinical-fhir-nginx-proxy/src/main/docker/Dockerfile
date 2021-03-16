FROM vasdvp/health-apis-centos:8

#INSTALL AWSCLI

RUN mkdir -p /tmp/aws
RUN for i in $(seq 10); do if curl -sSfkL "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o /tmp/aws/awscliv2.zip; then break; fi; sleep 3; done
RUN unzip -q /tmp/aws/awscliv2.zip -d /tmp/aws
RUN bash /tmp/aws/aws/install

#INSTALL NGINX

RUN retry yum install -y -q nginx

EXPOSE 9000

COPY entrypoint.sh /entrypoint.sh
ENTRYPOINT /entrypoint.sh
