

setwd(normalizePath(dirname(R.utils::commandArgs(asValues=TRUE)$"f")))
source('../../h2o-runit.R')

# use this for interactive setup
#      library(h2o)
#      library(testthat)
#      h2o.startLogging()
#      conn = h2o.init()


test.frame_add <- function(conn) {

    a_initial = cbind(
    v1=c(0,0,1,0,0,1,0,0,0,0),
    v2=c(1,1,1,0,1,0,1,0,1,0),
    v3=c(1,0,1,0,1,0,1,0,0,1),
    v4=c(1,1,0,0,0,1,0,0,0,1),
    v5=c(1,1,1,0,1,0,0,0,1,1),
    v6=c(1,0,1,0,0,0,0,0,1,1),
    v7=c(1,1,1,0,0,0,1,1,1,0),
    v8=c(0,0,1,1,1,0,0,1,1,0),
    v9=c(0,1,1,1,1,0,0,1,1,0),
    v10=c(0,0,0,0,0,1,1,0,0,0)
    )
    a <- a_initial
    b <- a_initial
    d <- a + b

    a.h2o <- as.h2o(conn, a_initial, destination_frame="cA_0")
    b.h2o <- as.h2o(conn, a_initial, destination_frame="cA_1")
    a.h2o + b.h2o
    d.h2o <- a.h2o + b.h2o
    d.h2o.R <- as.data.frame(d.h2o)
    expect_that(all(d == d.h2o.R), equals(T))

    a.h2o <- as.h2o(conn, a_initial, destination_frame="cA_2")
    b.h2o <- as.h2o(conn, a_initial, destination_frame="cA_3")
    a.h2o + b.h2o
    d.h2o <- a.h2o + b.h2o
    d.h2o.R <- as.data.frame(d.h2o)
    expect_that(all(d == d.h2o.R), equals(T))

    a.h2o <- as.h2o(conn, a_initial, destination_frame="cA_4")
    b.h2o <- as.h2o(conn, a_initial, destination_frame="cA_5")
    a.h2o + b.h2o
    d.h2o <- a.h2o + b.h2o
    d.h2o.R <- as.data.frame(d.h2o)
    expect_that(all(d == d.h2o.R), equals(T))

    a.h2o <- as.h2o(conn, a_initial, destination_frame="cA_6")
    b.h2o <- as.h2o(conn, a_initial, destination_frame="cA_7")
    a.h2o + b.h2o
    d.h2o <- a.h2o + b.h2o
    d.h2o.R <- as.data.frame(d.h2o)
    expect_that(all(d == d.h2o.R), equals(T))

    a.h2o <- as.h2o(conn, a_initial, destination_frame="cA_8")
    b.h2o <- as.h2o(conn, a_initial, destination_frame="cA_9")
    a.h2o + b.h2o
    d.h2o <- a.h2o + b.h2o
    d.h2o.R <- as.data.frame(d.h2o)
    expect_that(all(d == d.h2o.R), equals(T))

    testEnd()
}

doTest("Test frame add.", test.frame_add)


