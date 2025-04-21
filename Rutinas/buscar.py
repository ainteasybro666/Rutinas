import os
import logging
from termcolor import colored

# Configuración de logs
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(message)s')

def log_info(message):
    logging.info(colored(message, 'yellow'))

def log_error(message):
    logging.error(colored(message, 'red'))

def log_success(message):
    logging.info(colored(message, 'green'))

def main():
    project_root = os.path.join(os.path.expanduser("~"), "AndroidStudioProjects", "Rutinas")
    output_file = "archivos_concatenados.txt"
    files_to_process = []

    log_info("Bienvenido al script de extracción de archivos Kotlin.")
    
    # Verificar si el directorio raíz del proyecto existe
    if not os.path.exists(project_root):
        log_error(f"No se encontró la carpeta raíz del proyecto en la ruta: {project_root}")
        return
    
    log_info(f"Buscando archivos en: {project_root}")
    
    # Interacción con el usuario para ingresar los archivos
    while True:
        file_name = input("Ingresa el nombre del archivo Kotlin (ejemplo: archivo.kt) o 'kk' para finalizar: ")

        if file_name.lower() == "kk":
            if len(files_to_process) == 0:
                log_error("No se ha agregado ningún archivo a la lista. El proceso se detiene.")
                break
            else:
                log_info("Iniciando la búsqueda de archivos...")
                break
        
        # Validación del formato del archivo
        if not file_name.endswith(".kt"):
            log_error("Formato de archivo inválido. Asegúrate de ingresar un archivo con extensión .kt")
            continue
        
        # Buscar el archivo en todo el proyecto
        file_path = None
        for root, dirs, files in os.walk(project_root):
            if file_name in files:
                file_path = os.path.join(root, file_name)
                break
        
        if file_path:
            files_to_process.append(file_path)
            log_info(f"Archivo {file_name} encontrado y añadido a la lista.")
        else:
            log_error(f"El archivo {file_name} no existe en el proyecto. Verifica el nombre e intenta nuevamente.")
    
    if len(files_to_process) > 0:
        try:
            # Abrir el archivo de salida para escribir el código
            with open(output_file, "w", encoding="utf-8") as out_file:
                for file_path in files_to_process:
                    try:
                        with open(file_path, "r", encoding="utf-8") as f:
                            content = f.read()
                        
                        # Escribir encabezado con el nombre del archivo y el contenido
                        header = f"// {os.path.basename(file_path)}"
                        out_file.write(header + "\n\n")
                        out_file.write(content + "\n\n")
                        log_info(f"Contenido de {file_path} añadido al archivo de salida.")
                    except Exception as e:
                        log_error(f"Error al leer el archivo {file_path}: {e}")
            log_success(f"Archivo concatenado creado: {output_file}")
        
        except Exception as e:
            log_error(f"Error al abrir o escribir en el archivo {output_file}: {e}")

if __name__ == "__main__":
    main()
