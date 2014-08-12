#user.degree <- read.table("~/Documents/darshan-trace/2013-csr-db/user-degree.txt", quote="\"")
#job.degree <- read.table("~/Documents/darshan-trace/2013-csr-db/job-degree.txt", quote="\"")
process.degree <- read.table("~/Documents/darshan-trace/2013-csr-db/process-degree", quote="\"")

pdf(file="/Users/daidong/Documents/gitrepos/triton-private/papers/meta-graph/exps/pdegree.pdf",height=5, width=8,bg="white")
x = process.degree$V1
y = process.degree$V2

s = sum(y)
x2=seq(1, max(x), by=8)
y2=s*x2^(-2)
y3=s*x2^(-1)
y4=s*x2^(-0.5)

maxX=max(log10(x))
maxY=max(log10(y), log10(y2), log10(y3), log10(y4))

plot(log10(x2), log10(y2), type="l", col=2, lwd=3, yaxt="n", xaxt="n", bty="n", xlab="",ylab="", cex = .7, 
	xlim=c(0, maxX), ylim=c(0, maxY))
par(new=TRUE)
plot(log10(x2), log10(y3), type="l", col=3, lwd=3, yaxt="n", xaxt="n", bty="n", xlab="",ylab="", cex = .7,
	xlim=c(0, maxX), ylim=c(0, maxY))
par(new=TRUE)
plot(log10(x2), log10(y4), type="l", col=4, lwd=3, yaxt="n", xaxt="n", bty="n", xlab="",ylab="", cex = .7,
	xlim=c(0, maxX), ylim=c(0, maxY))
par(new=TRUE)

plot(log10(x), log10(y), xaxt="n", yaxt="n", xlab="Degree", ylab="Number of Vertices", xlim=c(0, maxX), ylim=c(0, maxY))

ticks=seq(0, maxX, by=1);
labels <- sapply(ticks, function(i) as.expression(bquote(10^ .(i))))
axis(1, at=ticks, labels=labels)

ticks=seq(0, maxY, by=1);
labels <- sapply(ticks, function(i) as.expression(bquote(10^ .(i))))
axis(2, at=ticks, labels=labels, las=1)

legend("topright", legend = c(expression(paste(alpha, " = 2")), 
	expression(paste(alpha, " = 1")), 
	expression(paste(alpha, " = 0.5"))), 
	lty=c(1,1,1),
	col=2:4)

invisible(dev.off())