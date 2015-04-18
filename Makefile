.PHONY: clean, pull, push, update
clean:
	rm -rf *.class
pull:
	git pull origin master
push:
	git push origin master
update: pull push
