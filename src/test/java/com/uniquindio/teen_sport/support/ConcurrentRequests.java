package com.uniquindio.teen_sport.support;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Lanza varias solicitudes a la vez, liberándolas juntas con un cerrojo, para probar escenarios de
 * concurrencia.
 */
public final class ConcurrentRequests {

    private ConcurrentRequests() {
    }

    /**
     * @param requests solicitudes que devuelven su estado HTTP
     * @return los estados, en el orden de las solicitudes
     * @throws Exception si alguna solicitud falla o tarda más de 30 segundos
     */
    @SafeVarargs
    public static List<Integer> run(Callable<Integer>... requests) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(requests.length);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (Callable<Integer> request : requests) {
                results.add(executor.submit(() -> {
                    start.await();
                    return request.call();
                }));
            }
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> result : results) {
                statuses.add(result.get(30, TimeUnit.SECONDS));
            }
            return statuses;
        } finally {
            executor.shutdownNow();
        }
    }
}
