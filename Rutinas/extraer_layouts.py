import os

def extraer_layouts():
    # Ruta relativa a la carpeta del proyecto
    ruta_layouts = os.path.join(os.getcwd(), 'app', 'src', 'main', 'res', 'layout')
    archivo_salida = 'todos_los_layouts.txt'

    if not os.path.isdir(ruta_layouts):
        print(f'No se encontró la carpeta: {ruta_layouts}')
        return

    with open(archivo_salida, 'w', encoding='utf-8') as salida:
        for root, _, files in os.walk(ruta_layouts):
            for file in files:
                if file.endswith('.xml'):
                    ruta_archivo = os.path.join(root, file)
                    try:
                        with open(ruta_archivo, 'r', encoding='utf-8') as f:
                            contenido = f.read()
                    except Exception as e:
                        print(f'Error al leer {ruta_archivo}: {e}')
                        continue

                    # Escribir el encabezado con el nombre del archivo
                    encabezado = f'// {file}'
                    salida.write(encabezado + '\n\n')
                    salida.write(contenido + '\n\n')

    print(f'Se ha creado el archivo: {archivo_salida}')

if __name__ == '__main__':
    extraer_layouts()
