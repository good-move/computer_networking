//
// Created by alexey on 11.11.17.
//

#ifndef MD5_BLOCKINGQUEUE_H
#define MD5_BLOCKINGQUEUE_H

#include <queue>
#include <pthread.h>
#include <climits>


template <class T>
class blocking_queue {

public:
    blocking_queue();
    blocking_queue(size_t max_size);
    ~blocking_queue();

    bool empty() const;
    size_t size() const;
    T& front();
    const T& front() const;
    T& back();
    const T& back() const;
    void push(const T& value);
    void pop();

private:

    size_t max_size = INT_MAX;
    std::queue<T> queue;
    pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;
    pthread_cond_t cond = PTHREAD_COND_INITIALIZER;

    int lock(pthread_mutex_t* mutex) const;
    int unlock(pthread_mutex_t* mutex) const;
    int notify_all(pthread_cond_t *mutex) const;

};


// *********************************************************************
// ************************** Implementation ***************************

template<class T>
blocking_queue<T>::
blocking_queue() {}

template<class T>
blocking_queue<T>::
blocking_queue(size_t max_size) {
  this->max_size = max_size;
}

template<class T>
bool
blocking_queue<T>::
empty() const {
  lock(&mutex);

  bool is_empty = queue.empty();

  unlock(&mutex);

  return is_empty;
}

template<class T>
size_t
blocking_queue<T>::
size() const {
  lock(&mutex);

  size_t size = queue.size();

  unlock(&mutex);

  return size;
}

template<class T>
T&
blocking_queue<T>::
front() {
  lock(&mutex);

  while(queue.empty()) {
    pthread_cond_wait(&cond, &mutex);
  }

  T& pointer = queue.front();

  notify_all(&cond);
  unlock(&mutex);

  return pointer;
}

template<class T>
const T&
blocking_queue<T>::
front() const {
  lock(&mutex);

  while(queue.empty()) {
    pthread_cond_wait(&cond, &mutex);
  }

  const T& reference = queue.front();

  notify_all(&cond);
  unlock(&mutex);

  return reference;
}

template<class T>
T&
blocking_queue<T>::
back() {
  lock(&mutex);

  while(queue.empty()) {
    pthread_cond_wait(&cond, &mutex);
  }

  T& pointer = queue.back();

  notify_all(&cond);
  unlock(&mutex);

  return pointer;
}

template<class T>
const T&
blocking_queue<T>::
back() const {
  lock(&mutex);

  while(queue.empty()) {
    pthread_cond_wait(&cond, &mutex);
  }

  const T& pointer = queue.back();

  notify_all(&cond);
  unlock(&mutex);

  return pointer;
}

template <class T>
void
blocking_queue<T>::
push(const T &value) {
  lock(&mutex);

  while(queue.size() == this->max_size) {
    pthread_cond_wait(&cond, &mutex);
  }

  queue.push(value);

  notify_all(&cond);
  unlock(&mutex);
}

template <class T>
void
blocking_queue<T>::
pop() {
  lock(&mutex);

  while(queue.size() == this->max_size) {
    pthread_cond_wait(&cond, &mutex);
  }

  queue.pop();

  notify_all(&cond);
  unlock(&mutex);
}


template <class T>
int
blocking_queue<T>::
lock(pthread_mutex_t* mutex) const {
  return pthread_mutex_lock(mutex);
}

template <class T>
int
blocking_queue<T>::
unlock(pthread_mutex_t *mutex) const {
  return pthread_mutex_unlock(mutex);
}

template <class T>
int
blocking_queue<T>::
notify_all(pthread_cond_t *cond) const {
  return pthread_cond_broadcast(cond);
}

template<class T>
blocking_queue<T>::
~blocking_queue() {
  pthread_cond_destroy(&cond);
  pthread_mutex_destroy(&mutex);
}

#endif //MD5_BLOCKINGQUEUE_H
