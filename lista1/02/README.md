(dev) CountDownLatch é um sincronizador disponível na sdk Java (https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/CountDownLatch.html). Faça uma nova implementação da mesma ideia usando somente variáveis condicionais (wait, notify e notifyAll) e a construção synchronized. Se precisar usar alguma coleção, use uma LinkedList ou ArrayList. Só é necessário implementar a API abaixo:
void await()
void countDown()

Caso ainda tenha dúvidas sobre a semântica da CountDownLatch após ler sua documentação, procure o professor para tirar as dúvidas.
