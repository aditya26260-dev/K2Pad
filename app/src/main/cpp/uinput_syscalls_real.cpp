#include "uinput_syscalls.h"

#include <fcntl.h>
#include <sys/ioctl.h>
#include <unistd.h>

namespace k2pad {
namespace {

int real_ioctl_int(int fd, unsigned long request, int value) {
    return ::ioctl(fd, request, value);
}

int real_ioctl_ptr(int fd, unsigned long request, const void* data) {
    return ::ioctl(fd, request, data);
}

ssize_t real_write(int fd, const void* buf, size_t count) {
    return ::write(fd, buf, count);
}

int real_open(const char* path, int flags) {
    return ::open(path, flags);
}

int real_close(int fd) {
    return ::close(fd);
}

}  // namespace

const UinputSyscalls* real_uinput_syscalls() {
    static const UinputSyscalls instance = {
        real_ioctl_int,
        real_ioctl_ptr,
        real_write,
        real_open,
        real_close,
    };
    return &instance;
}

}  // namespace k2pad
