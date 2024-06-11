# Utiliser l'image de base OpenJDK 21
FROM openjdk:21

# Définir le répertoire de travail dans le conteneur
WORKDIR /app

# Copier les fichiers Java et le fichier API_Key.txt dans le conteneur
COPY ProxyServer.java ChatGPTClient.java API_Key.txt /app/

# Compiler les fichiers Java
RUN javac *.java

# Exposer le port sur lequel le serveur proxy écoute (ajustez selon votre configuration)
EXPOSE 8080

# Définir la commande à exécuter lorsque le conteneur démarre
CMD ["java", "ProxyServer"]
