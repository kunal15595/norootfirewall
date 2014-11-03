/*
 * sys_arch.h
 *
 *  Created on: Nov 3, 2014
 *      Author: maksim-dmitriev
 */

#ifndef LWIP_1_4_1_SRC_INCLUDE_SYS_ARCH_H_
#define LWIP_1_4_1_SRC_INCLUDE_SYS_ARCH_H_

#include "cc.h"


//typedef int sys_sem_t;

sys_sem_t sys_sem_new(u8_t count);
void sys_sem_free(sys_sem_t sem);
void sys_sem_signal(sys_sem_t sem);
u32_t sys_arch_sem_wait(sys_sem_t sem, u32_t timeout);
sys_mbox_t sys_mbox_new(int size);
void sys_mbox_free(sys_mbox_t mbox);
void sys_mbox_post(sys_mbox_t mbox, void *msg);
u32_t sys_arch_mbox_fetch(sys_mbox_t mbox, void **msg, u32_t timeout);
u32_t sys_arch_mbox_tryfetch(sys_mbox_t mbox, void **msg);
err_t sys_mbox_trypost(sys_mbox_t mbox, void *msg);

#define SYS_ARCH_DECL_PROTECT(lev) sys_prot_t lev
#define SYS_ARCH_PROTECT(lev) lev = sys_arch_protect()
#define SYS_ARCH_UNPROTECT(lev) sys_arch_unprotect(lev)

#endif /* LWIP_1_4_1_SRC_INCLUDE_SYS_ARCH_H_ */
