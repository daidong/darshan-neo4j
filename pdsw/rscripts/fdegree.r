#user.degree <- read.table("~/Documents/darshan-trace/2013-csr-db/user-degree.txt", quote="\"")
#job.degree <- read.table("~/Documents/darshan-trace/2013-csr-db/job-degree.txt", quote="\"")
#process.degree <- read.table("~/Documents/darshan-trace/2013-csr-db/process-degree", quote="\"")
file.degree <- read.table("~/Documents/darshan-trace/2013-csr-db/file-degree", quote="\"")

pdf(file="/Users/daidong/Documents/gitrepos/triton-private/papers/meta-graph/exps/fdegree.pdf",height=5, width=8,bg="white")
x = file.degree$V1
y = file.degree$V2

s = sum(y)
x2=seq(1, max(x), by=8)
y2=s*x2^(-4)
y3=s*x2^(-2)
y4=s*x2^(-1.2)

plot(log10(x2), log10(y2), type="l", col=2, lwd=3, yaxt="n", xaxt="n", bty="n", xlab="",ylab="", cex = .7, 
	xlim=c(0, 7), ylim=c(0, 8))
par(new=TRUE)
plot(log10(x2), log10(y3), type="l", col=3, lwd=3, yaxt="n", xaxt="n", bty="n", xlab="",ylab="", cex = .7,
	xlim=c(0, 7), ylim=c(0, 8))
par(new=TRUE)
plot(log10(x2), log10(y4), type="l", col=4, lwd=3, yaxt="n", xaxt="n", bty="n", xlab="",ylab="", cex = .7,
	xlim=c(0, 7), ylim=c(0, 8))
par(new=TRUE)

plot(log10(x), log10(y), xaxt="n", yaxt="n", xlab="Degree", ylab="Number of Vertices", xlim=c(0, 7), ylim=c(0, 8))

ticks=seq(0, 7, by=1);
labels <- sapply(ticks, function(i) as.expression(bquote(10^ .(i))))
axis(1, at=c(0,1,2,3,4,5,6,7), labels=labels)

ticks=seq(0, 8, by=1);
labels <- sapply(ticks, function(i) as.expression(bquote(10^ .(i))))
axis(2, at=c(0,1,2,3,4,5,6,7,8), labels=labels, las=1)

legend("topright", legend = c(expression(paste(alpha, " = 4")), 
	expression(paste(alpha, " = 2")), 
	expression(paste(alpha, " = 1"))), 
	lty=c(1,1,1),
	col=2:4,
	cex=1.5)

invisible(dev.off())