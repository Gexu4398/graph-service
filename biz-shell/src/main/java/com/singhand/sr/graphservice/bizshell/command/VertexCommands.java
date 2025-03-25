package com.singhand.sr.graphservice.bizshell.command;

import com.singhand.sr.graphservice.bizshell.service.VertexCommandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

@ShellComponent
public class VertexCommands {

  private final VertexCommandService vertexCommandService;

  @Autowired
  public VertexCommands(VertexCommandService vertexCommandService) {

    this.vertexCommandService = vertexCommandService;
  }

  @ShellMethod(key = "output-vertex", value = "Output vertex", group = "Output resources")
  public void outputVertex(@ShellOption(
          value = {"-t", "--output-type"},
          help = "Specify the type of the vertex to be output") String type,
      @ShellOption(
          value = {"-o", "--output-directory"},
          help = "The directory where output files to place") String outputDirectory) {

    vertexCommandService.outputVertex(type, outputDirectory);
  }

  @ShellMethod(key = "output-vertex-excel", value = "Output vertex to excel", group = "Output resources")
  public void outputVertexToExcel(@ShellOption(
          value = {"-t", "--output-type"},
          help = "Specify the type of the vertex to be output") String type,
      @ShellOption(
          value = {"-o", "--output-directory"},
          help = "The directory where output files to place") String outputDirectory) {

    vertexCommandService.outputVertexToExcel(type, outputDirectory);
  }

  @ShellMethod(key = "file-vertex-to-json", value = "File vertex to json", group = "Output resources")
  public void fileVertexToJson(@ShellOption(
      value = {"-o", "--output-directory"},
      help = "The directory where input files to place") String inputDirectory) {

    vertexCommandService.fileVertexToJson(inputDirectory);
  }
}
