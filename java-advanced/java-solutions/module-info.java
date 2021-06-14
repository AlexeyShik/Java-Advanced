/**
 * Module with all solutions for <a href="https://www.kgeorgiy.info/courses/java-advanced/">Java Advanced</a> course
 */
module solutions {
    requires java.compiler;
    requires java.rmi;
    requires junit;
    requires jsoup;

    requires info.kgeorgiy.java.advanced.base;
    requires info.kgeorgiy.java.advanced.walk;
    requires info.kgeorgiy.java.advanced.arrayset;
    requires info.kgeorgiy.java.advanced.student;
    requires info.kgeorgiy.java.advanced.implementor;
    requires info.kgeorgiy.java.advanced.concurrent;
    requires info.kgeorgiy.java.advanced.mapper;
    requires info.kgeorgiy.java.advanced.crawler;
    requires info.kgeorgiy.java.advanced.hello;
    requires jdk.httpserver;

    exports info.kgeorgiy.ja.shik.implementor;
    exports info.kgeorgiy.ja.shik.bank;
    exports info.kgeorgiy.ja.shik.bank.tests to junit;
    exports info.kgeorgiy.ja.shik.i18n.tests to junit;
    opens info.kgeorgiy.ja.shik.implementor;
}