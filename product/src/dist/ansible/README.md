# Ansible

## Quick Start
- Install ansible
    - `apt-get install ansible` or `yum install ansible`
    
- Edit `inventory` and add your hosts

- Edit `codekvast-daemon.yml` to suit your needs

- Run the playbook:
    - `ansible-playbook -i inventory codekvast-daemon.yml`

`inventory` is a text file listing the servers
