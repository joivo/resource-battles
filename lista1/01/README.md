# First Question

Em sala, discutimos uma implementação de um lock justo, em clang. Esse lock usava uma fila para manter identificadores de
pthreads em espera para executar a região crítica. Uma vez liberado o lock, a thread que entrou primeiro na fila deve ser
escolhida para executar. 

Faça uma implementação em Java com as mesmas características, seguindo a interface listada abaixo.

Sua implementação não pode: 

    - usar os métodos wait, notify e notifyAll da classe Object. 
    - usar synchronized na declaração de nenhum método criado por você. 
    - usar nenhum objeto do pacote java.util.concurrent <strong> exceto </strong> java.util.concurrent.locks.LockSupport.park() 
    para bloquear a execução da Thread corrente e java.util.concurrent.locks.LockSupport.unpark(Thread thread) para desbloquear.

Use ArrayList para implementar a fila.
