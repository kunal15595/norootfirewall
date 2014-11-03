#include <semaphore.h>
#include "sys_arch.h"

/*
 * Creates and returns a new semaphore. The count argument specifies the initial state of the
 * semaphore. Returns the semaphore, or SYS_SEM_NULL on error.
 */
sys_sem_t sys_sem_new(u8_t count) {

}

void sys_sem_free(sys_sem_t sem) {

}

void sys_sem_signal(sys_sem_t sem) {

}

u32_t sys_arch_sem_wait(sys_sem_t sem, u32_t timeout) {

}

sys_mbox_t sys_mbox_new(int size) {

}

void sys_mbox_free(sys_mbox_t mbox) {

}

void sys_mbox_post(sys_mbox_t mbox, void *msg) {

}

u32_t sys_arch_mbox_fetch(sys_mbox_t mbox, void **msg, u32_t timeout) {

}

u32_t sys_arch_mbox_tryfetch(sys_mbox_t mbox, void **msg) {

}

err_t sys_mbox_trypost(sys_mbox_t mbox, void *msg) {

}
