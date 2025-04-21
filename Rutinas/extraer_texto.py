import os

def main():
    # Ruta relativa a la carpeta de proyecto
    target_dir = os.path.join(os.getcwd(), "app", "src", "main", "java", "com", "example", "rutinas")
    output_file = "all_kotlin_code.txt"

    if not os.path.isdir(target_dir):
        print(f"No se encontró la carpeta destino: {target_dir}")
        return

    with open(output_file, "w", encoding="utf-8") as out_file:
        # Recorremos de forma recursiva la carpeta
        for root, dirs, files in os.walk(target_dir):
            for file in files:
                if file.endswith(".kt"):
                    file_path = os.path.join(root, file)
                    try:
                        with open(file_path, "r", encoding="utf-8") as f:
                            content = f.read()
                    except Exception as e:
                        print(f"Error al leer {file_path}: {e}")
                        continue

                    # Encabezado esperado: //nombredelarchivo
                    header = f"//{file}"
                    if not content.lstrip().startswith(header):
                        out_file.write(header + "\n\n")
                    out_file.write(content + "\n\n")
    
    print(f"Archivo concatenado creado: {output_file}")

if __name__ == "__main__":
    main()
