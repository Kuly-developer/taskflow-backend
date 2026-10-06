package com.example.taskflow_backend;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Tuple;

public class MainVerticle extends AbstractVerticle {

    private Pool pgPool;

    @Override
    public void start(Promise<Void> startPromise) throws Exception {

        PgConnectOptions connectOptions = new PgConnectOptions()
            .setPort(5432)
            .setHost("localhost")
            .setDatabase("Landing-Page") // የዳታቤዝህ ስም
            .setUser("postgres")
            .setPassword("12345"); // የዳታቤዝህ ፓስወርድ

        PoolOptions poolOptions = new PoolOptions().setMaxSize(5);
        pgPool = Pool.pool(vertx, connectOptions, poolOptions);

        Router router = Router.router(vertx);
        router.route().handler(CorsHandler.create()
            .allowedMethod(HttpMethod.POST)
            .allowedMethod(HttpMethod.GET)
            .allowedHeader("Content-Type"));

        router.route().handler(BodyHandler.create());

        router.post("/api/waitlist").handler(ctx -> {
            String email = ctx.body().asJsonObject().getString("email");
            System.out.println("email is delivered: " + email);

            String sqlQuery = "INSERT INTO waitlist (email) VALUES ($1)";

            pgPool.preparedQuery(sqlQuery)
                .execute(Tuple.of(email))
                .onComplete(ar -> {
                    if (ar.succeeded()) {
                        // 🟢 ዳታቤዝ ላይ አዲስ ኢሜይል በሰላም ሲቀመጥ (Status: 200)
                        ctx.response()
                            .putHeader("content-type", "application/json")
                            .setStatusCode(200)
                            .end("{\"message\":\"ኢሜይሉ በPostgreSQL ዳታቤዝ ውስጥ በስኬት ተቀምጧል!\"}");
                    } else {
                        // 🔴 ተመሳሳይ ኢሜይል ሲሆን ዳታቤዙ እምቢ ይላል (Status: 400)
                        System.out.println("❌ database error: " + ar.cause().getMessage());
                        ctx.response()
                            .putHeader("content-type", "application/json")
                            .setStatusCode(400) // 👈 ይህንን ስህተት ለAngular ያስረክባል
                            .end("{\"error\":\"this email is already registered::\"}");
                    }
                });

            // ❌ እዚህ ቦታ ላይ የነበረው ትርፍ የ ctx.response() ኮድ ሙሉ በሙሉ ጠፍቷል!
            // ምክንያቱም ምላሹ መላክ ያለበት ከዳታቤዝ ውጤት በኋላ ብቻ ነው!
        });

        vertx.createHttpServer()
            .requestHandler(router)
            .listen(8080)
            .onComplete(http -> {
                if (http.succeeded()) {
                    startPromise.complete();
                    System.out.println("🚀 TaskFlow java server port 8080 is started successfully!");
                } else {
                    startPromise.fail(http.cause());
                }
            });
    }
}
