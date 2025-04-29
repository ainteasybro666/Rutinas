import os
import logging
from termcolor import colored

# Configuración de logs
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(message)s')

# Variable global para el modo de salida
output_mode = "si"  # Valor por defecto: 'si' (sin imports)

def log_info(message):
    logging.info(colored(message, 'yellow'))

def log_error(message):
    logging.error(colored(message, 'red'))

def log_success(message):
    logging.info(colored(message, 'green'))

def show_commands():
    log_info("Comandos disponibles:")
    log_info(" - si: Sin imports (por defecto)")
    log_info(" - cc: Código completo (incluye imports y package)")
    log_info(" - sip: Sin imports ni package")

def process_file_content(file_path):
    """Procesa el contenido del archivo según el modo seleccionado"""
    try:
        with open(file_path, "r", encoding="utf-8") as f:
            content = f.read()

        # Dependiendo del modo, modificamos el contenido
        if output_mode == "cc":
            # Modo completo: incluir imports y package
            header = f"// {os.path.basename(file_path)}"
            content = f"package my.package.name\n\n" + content  # Ejemplo de package
            content = f"import java.util.*;\n\n" + content  # Ejemplo de imports
        elif output_mode == "sip":
            # Modo sin imports ni package
            header = f"// {os.path.basename(file_path)}"
            # Simplemente se omite imports y package
            content = "\n".join(line for line in content.splitlines() if not line.startswith("import") and not line.startswith("package"))
        else:
            # Modo por defecto: solo el contenido del archivo, sin imports
            header = f"// {os.path.basename(file_path)}"
            content = "\n".join(line for line in content.splitlines() if not line.startswith("import"))

        return header, content

    except Exception as e:
        log_error(f"Error al leer el archivo {file_path}: {e}")
        return None, None

def main():
    global output_mode
    project_root = os.path.join(os.path.expanduser("~"), "AndroidStudioProjects", "Rutinas")
    output_file = "archivos_concatenados.txt"
    files_to_process = []

    log_info("Bienvenido al script de extracción de archivos Kotlin.")
    show_commands()
    log_info(f"Modo actual: {output_mode}")
    
    # Verificar si el directorio raíz del proyecto existe
    if not os.path.exists(project_root):
        log_error(f"No se encontró la carpeta raíz del proyecto en la ruta: {project_root}")
        return
    
    log_info(f"Buscando archivos en: {project_root}")
    
    # Interacción con el usuario para ingresar los archivos o comandos
    while True:
        user_input = input("Ingresa un archivo Kotlin (ejemplo: archivo.kt), un comando o 'kk' para finalizar: ").strip()

        if user_input.lower() == "kk":
            if len(files_to_process) == 0:
                log_error("No se ha agregado ningún archivo a la lista. El proceso se detiene.")
                break
            else:
                log_info("Iniciando la búsqueda de archivos...")
                break
        
        elif user_input.lower() == "-cc":
            output_mode = "cc"
            log_info(f"Modo de salida cambiado a: {output_mode}")

        elif user_input.lower() == "-sip":
            output_mode = "sip"
            log_info(f"Modo de salida cambiado a: {output_mode}")

        elif user_input.lower() == "-si":
            output_mode = "si"
            log_info(f"Modo de salida cambiado a: {output_mode}")

        else:
            # Si no es un comando, tratamos de buscar el archivo
            file_name = user_input
            # Asegurarse de que tenga la extensión .kt
            if not file_name.endswith(".kt"):
                file_name += ".kt"

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
    
    # Procesar los archivos seleccionados
    if len(files_to_process) > 0:
        try:
            # Abrir el archivo de salida para escribir el código
            with open(output_file, "w", encoding="utf-8") as out_file:
                for file_path in files_to_process:
                    try:
                        # Procesamos el contenido del archivo según el modo seleccionado
                        header, content = process_file_content(file_path)
                        if header and content:
                            # Escribir encabezado con el nombre del archivo y el contenido
                            out_file.write(header + "\n\n")
                            out_file.write(content + "\n\n")
                            log_info(f"Contenido de {file_path} añadido al archivo de salida.")
                    except Exception as e:
                        log_error(f"Error al procesar el archivo {file_path}: {e}")
            log_success(f"Archivo concatenado creado: {output_file}")
        
        except Exception as e:
            log_error(f"Error al abrir o escribir en el archivo {output_file}: {e}")

if __name__ == "__main__":
    main()
