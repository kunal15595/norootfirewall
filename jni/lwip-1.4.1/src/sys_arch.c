void sys_init(void)
{
}

err_t sys_sem_new(sys_sem_t *sem, u8_t count)
{
	sem_init(sem, count);
	return ERR_OK;
}

void sys_sem_free(sys_sem_t * sem)
{
	sem_destroy(sem);
}

void sys_sem_signal(sys_sem_t * sem)
{
	sem_post(sem);
}

u32_t sys_arch_sem_wait(sys_sem_t * sem, u32_t timeout)
{
	lk_time_t start = current_time();

	status_t err = sem_timedwait(sem, timeout ? timeout : INFINITE_TIME);
	if (err == ERR_TIMED_OUT)
		return SYS_ARCH_TIMEOUT;
	
	return current_time() - start;
}

int sys_sem_valid(sys_sem_t *sem)
{
	return 1;
}

void sys_sem_set_invalid(sys_sem_t *sem)
{
}

err_t sys_mbox_new(sys_mbox_t * mbox, int size)
{
	sem_init(&mbox->empty, size);
	sem_init(&mbox->full, 0);
	mutex_init(&mbox->lock);

	mbox->head = 0;
	mbox->tail = 0;
	mbox->size = size;

	mbox->queue = calloc(size, sizeof(void *));
	if (!mbox->queue)
		return ERR_MEM;

	return ERR_OK;
}

void sys_mbox_free(sys_mbox_t *mbox)
{
	free(mbox->queue);
	mbox->queue = NULL;
}
