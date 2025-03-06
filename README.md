***Практически сырой проект для разработки плагина под paper 1.21.4***


**Требования к системе**
- Установленный java 21 версии
- Maven под 21 версию java
**Для Mac OS**
```path
sh install_java_maven.sh
```


***ГАЙД ПО ИНИЦИАЛИЗАЦИИ ПРОЕКТА***

**Для Mac OS**
1. Перейдите в директорию плагина
```path
cd minecraft/dev
```
2. Инициализируйте проект
```path
sh init_project.sh
```
3. Соберите проект
```path
sh build_and_deploy.sh
```
4. В другой консоле перейдите в директорию тестового сервера
```path
cd minecraft/paper_server/
```
5. Создайте файл eula.txt с строкой eula=true
6. Запустите сервер
```path
sh start.sh
```