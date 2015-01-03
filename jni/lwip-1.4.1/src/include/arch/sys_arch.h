/*
 * sys_arch.h
 *
 *  Created on: Nov 3, 2014
 *      Author: maksim-dmitriev
 */

#ifndef LWIP_1_4_1_SRC_INCLUDE_SYS_ARCH_H_
#define LWIP_1_4_1_SRC_INCLUDE_SYS_ARCH_H_

#include <errno.h>

#define SYS_MBOX_NULL NULL
#define SYS_SEM_NULL  NULL

typedef u32_t sys_prot_t;

struct sys_sem;
typedef struct sys_sem * sys_sem_t;
#define sys_sem_valid(sem) (((sem) != NULL) && (*(sem) != NULL))
#define sys_sem_set_invalid(sem) do { if((sem) != NULL) { *(sem) = NULL; }}while(0)

/* let sys.h use binary semaphores for mutexes */
#define LWIP_COMPAT_MUTEX 1

struct sys_mbox;
typedef struct sys_mbox *sys_mbox_t;
#define sys_mbox_valid(mbox) (((mbox) != NULL) && (*(mbox) != NULL))
#define sys_mbox_set_invalid(mbox) do { if((mbox) != NULL) { *(mbox) = NULL; }}while(0)

struct sys_thread;
typedef struct sys_thread * sys_thread_t;

#endif /* LWIP_1_4_1_SRC_INCLUDE_SYS_ARCH_H_ */

