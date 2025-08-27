FROM artifactory-service-address/path-to-java-image

RUN mkdir /var/run/sshd
RUN echo 'root:test_password' | chpasswd
RUN sed -i 's/PermitRootLogin prohibit-password/PermitRootLogin yes/' /etc/ssh/sshd_config

ENV NOTVISIBLE "in users profile"
RUN echo "export VISIBLE=now" >> /etc/profile

RUN ssh-keygen -P "" -t rsa -f /etc/ssh/ssh_host_rsa_key
RUN ssh-keygen -P "" -t ecdsa -f /etc/ssh/ssh_host_ecdsa_key
RUN ssh-keygen -P "" -t ed25519 -f /etc/ssh/ssh_host_ed25519_key
RUN ssh-keygen -P "" -t dsa -f /etc/ssh/ssh_host_dsa_key

EXPOSE 22
CMD ["/usr/sbin/sshd", "-D"]