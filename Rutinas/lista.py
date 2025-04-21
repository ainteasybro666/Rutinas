import os

def list_directory_tree(base_path, output_file):
    print(f"Listando directorio: {base_path}")  # Log para rastrear el directorio actual
    # Recorremos el directorio de manera recursiva
    for root, dirs, files in os.walk(base_path):
        # Nivel de profundidad
        level = root.replace(base_path, '').count(os.sep)
        # Se calcula el prefijo para indicar el nivel de directorio
        indent = ' ' * 4 * level
        if level == 0:
            indent = ''  # No se usa sangría para la raíz

        print(f"Procesando carpeta: {root}")  # Log para mostrar la carpeta procesada

        # Escribir el directorio actual
        output_file.write(f"{indent}{os.path.basename(root)}/\n")
        
        if not files:
            print(f"No se encontraron archivos en {root}")  # Log para cuando una carpeta está vacía
        else:
            # Listar los archivos dentro de este directorio
            for file in files:
                print(f"  Encontrado archivo: {file}")  # Log para mostrar archivos encontrados
                output_file.write(f"{indent}    {file}\n")

def main():
    # Definir las rutas de los directorios de interés
    project_dir = os.path.join(os.getcwd(), "app", "src", "main")
    java_dir = os.path.join(project_dir, "java")
    res_dir = os.path.join(project_dir, "res")
    
    # Nombre del archivo de salida
    output_file = "estructura_directorio.txt"

    # Comprobamos si las carpetas java y res existen
    if not os.path.isdir(java_dir):
        print(f"No se encontró la carpeta Java en: {java_dir}")
    else:
        print(f"Carpeta Java encontrada en: {java_dir}")
    
    if not os.path.isdir(res_dir):
        print(f"No se encontró la carpeta RES en: {res_dir}")
    else:
        print(f"Carpeta RES encontrada en: {res_dir}")
    
    # Comprobamos que las carpetas existan antes de proceder
    if not os.path.isdir(java_dir) or not os.path.isdir(res_dir):
        print("Una o ambas carpetas no existen. Terminando ejecución.")
        return

    with open(output_file, "w", encoding="utf-8") as output_file:
        # Escribir la sección de Java
        output_file.write("### Estructura de Archivos en Java ###\n")
        list_directory_tree(java_dir, output_file)
        
        # Escribir un separador entre secciones
        output_file.write("\n### Estructura de Archivos en RES ###\n")
        list_directory_tree(res_dir, output_file)

    print(f"Archivo de estructura de directorio creado: {output_file}")

if __name__ == "__main__":
    main()
