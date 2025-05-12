import os
import logging
import pyperclip
from termcolor import colored
import platform
import subprocess

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

def format_filename_title(filename):
    name, ext = os.path.splitext(filename)
    parts = name.replace("_", " ").replace("-", " ").split()
    title_case = ''.join(word.capitalize() for word in parts)
    return f"// {title_case}{ext}"

def process_file_content(file_path):
    """Procesa el contenido del archivo según el modo seleccionado"""
    try:
        with open(file_path, "r", encoding="utf-8") as f:
            content = f.read()

        ext = os.path.splitext(file_path)[1].lower()
        header = format_filename_title(os.path.basename(file_path))

        if ext == ".kt":
            if output_mode == "cc":
                content = f"package my.package.name\n\nimport java.util.*;\n\n{content}"
            elif output_mode == "sip":
                content = "\n".join(line for line in content.splitlines() if not line.startswith("import") and not line.startswith("package"))
            else:  # modo "si"
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

    log_info("Bienvenido al script de extracción de archivos Kotlin y XML.")
    show_commands()
    log_info(f"Modo actual: {output_mode}")
    
    if not os.path.exists(project_root):
        log_error(f"No se encontró la carpeta raíz del proyecto en la ruta: {project_root}")
        return
    
    log_info(f"Buscando archivos en: {project_root}")
    
    while True:
        user_input = input("Ingresa un archivo Kotlin o XML, un comando, o 'v' (ver), 'c' (copiar) para finalizar: ").strip()

        if user_input.lower() in ("v", "c"):
            if not files_to_process:
                log_error("No se ha agregado ningún archivo a la lista. El proceso se detiene.")
                break
            else:
                log_info("Procesando archivos seleccionados...")
                break

        elif user_input == "-cc":
            output_mode = "cc"
            log_info(f"Modo de salida cambiado a: {output_mode}")

        elif user_input == "-sip":
            output_mode = "sip"
            log_info(f"Modo de salida cambiado a: {output_mode}")

        elif user_input == "-si":
            output_mode = "si"
            log_info(f"Modo de salida cambiado a: {output_mode}")

        else:
            filename = user_input

            if not filename.endswith(".kt") and not filename.endswith(".xml"):
                # Probaremos ambos
                possible_extensions = [".kt", ".xml"]
            else:
                possible_extensions = [""]
            
            found = False

            for ext in possible_extensions:
                search_name = filename if ext == "" else filename + ext
                file_path = None

                for root, dirs, files in os.walk(project_root):
                    if any(file.lower() == search_name.lower() for file in files):
                        file_path = os.path.join(root, search_name)
                        found = True
                        break
                if found:
                    break

            if found and file_path:
                files_to_process.append(file_path)
                log_info(f"Archivo {os.path.basename(file_path)} encontrado y añadido a la lista.")
            else:
                log_error(f"El archivo '{filename}' no se encontró en el proyecto. Verifica el nombre.")

    if files_to_process:
        try:
            with open(output_file, "w", encoding="utf-8") as out_file:
                for file_path in files_to_process:
                    header, content = process_file_content(file_path)
                    if header and content:
                        out_file.write(header + "\n\n")
                        out_file.write(content + "\n\n")
                        log_info(f"Contenido de {file_path} añadido al archivo de salida.")
            log_success(f"Archivo concatenado creado: {output_file}")

            if user_input.lower() == "v":
                log_info("Abriendo archivo generado...")
                if platform.system() == "Windows":
                    os.startfile(output_file)
                elif platform.system() == "Darwin":
                    subprocess.call(["open", output_file])
                else:
                    subprocess.call(["xdg-open", output_file])
            elif user_input.lower() == "c":
                with open(output_file, "r", encoding="utf-8") as out_file:
                    content_to_copy = out_file.read()
                    pyperclip.copy(content_to_copy)
                    log_success("Contenido copiado al portapapeles.")

        except Exception as e:
            log_error(f"Error durante la escritura o acción final: {e}")

if __name__ == "__main__":
    main()
