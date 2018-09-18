## swapfile

[![Build Status](https://travis-ci.org/Oefenweb/ansible-swapfile.svg?branch=master)](https://travis-ci.org/Oefenweb/ansible-swapfile) [![Ansible Galaxy](http://img.shields.io/badge/ansible--galaxy-swapfile-blue.svg)](https://galaxy.ansible.com/Oefenweb/swapfile)

Ansible role to manage a swap file in Debian-like systems.

#### Requirements

* `fallocate` (will be installed)

## Variables

* `swapfile_size`: [default: `1G`, `false` to do nothing]: The size of the swap file to create in the format that `fallocate` expects: The length and offset arguments may be followed by binary (2^N) suffixes KiB, MiB, GiB, TiB, PiB and EiB (the "iB" is optional, e.g. "K" has the same meaning as "KiB") or decimal (10^N) suffixes KB, MB, GB, PB and EB.
* `swapfile_swappiness`: [optional]: The swappiness percentage (`vm.swappiness`) -- the lower it is, the less your system swaps memory pages
* `swapfile_vfs_cache_pressure`: [optional]: This percentage value controls the tendency of the kernel to reclaim the memory which is used for caching of directory and inode objects

## Dependencies

None

#### Example

```yaml
- hosts: all
  roles:
    - swapfile
```

or:

```yaml
- hosts: all
  roles:
    - role: swapfile
      swapfile_size: 1GB
      swapfile_swappiness: 10
```

#### License

MIT

#### Author Information

Mischa ter Smitten (based on work of [kamaln7](https://github.com/kamaln7))

#### Feedback, bug-reports, requests, ...

Are [welcome](https://github.com/Oefenweb/ansible-swapfile/issues)!
