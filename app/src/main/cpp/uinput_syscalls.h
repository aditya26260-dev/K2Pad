#ifndef K2PAD_UINPUT_SYSCALLS_H
#define K2PAD_UINPUT_SYSCALLS_H

#include <sys/types.h>

namespace k2pad {

// Every implementation (real or fake) MUST follow POSIX convention on
// failure: return -1 and set errno. uinput_backend.cpp relies on this to
// build a meaningful, non-swallowed error via `return -errno;` (project
// brief section 29).
//
// A plain struct of function pointers rather than a virtual-method
// interface: it stays trivially usable from both production JNI code and
// from a plain-C-style test fake with no vtable/ABI concerns, and swapping
// implementations is just swapping which const instance gets passed in.
struct UinputSyscalls {
    int (*ioctl_int)(int fd, unsigned long request, int value);
    int (*ioctl_ptr)(int fd, unsigned long request, const void* data);
    ssize_t (*write_bytes)(int fd, const void* buf, size_t count);
    int (*open_path)(const char* path, int flags);
    int (*close_fd)(int fd);
};

// The real implementation, backed by actual libc open/ioctl/write/close.
// This is what all production (JNI) call sites use.
const UinputSyscalls* real_uinput_syscalls();

}  // namespace k2pad

#endif  // K2PAD_UINPUT_SYSCALLS_H
