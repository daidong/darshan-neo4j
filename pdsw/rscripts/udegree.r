user.degree <- read.table("~/Documents/darshan-trace/2013-csr-db/user-degree.txt", quote="\"")
#job.degree <- read.table("~/Documents/darshan-trace/2013-csr-db/job-degree.txt", quote="\"")
#process.degree <- read.table("~/Documents/darshan-trace/2013-csr-db/process-degree", quote="\"")

pdf(file="/Users/daidong/Documents/gitrepos/triton-private/papers/meta-graph/exps/udegree.pdf",height=5, width=8,bg="white")
x = user.degree$V1
y = user.degree$V2

s = sum(y)

maxX=max(log10(x))
maxY=max(log10(y))

plot(log10(x), log10(y), xaxt="n", yaxt="n", xlab="Degree", ylab="Number of Vertices", xlim=c(0, maxX), ylim=c(0, maxY))

ticks=seq(0, maxX, by=1);
labels <- sapply(ticks, function(i) as.expression(bquote(10^ .(i))))
axis(1, at=ticks, labels=labels)

ticks=seq(0, maxY, by=1);
labels <- sapply(ticks, function(i) as.expression(bquote(10^ .(i))))
axis(2, at=ticks, labels=labels, las=1)

invisible(dev.off())