package br.com.zup.edu

import com.google.protobuf.Any
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.protobuf.StatusProto
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.exceptions.HttpStatusException
import javax.inject.Inject

@Controller
class CalculadoraDeFretesController(
    @Inject val gRpcClient: FretesServiceGrpc.FretesServiceBlockingStub
) {
    @Get("/api/oi")
    fun oi(): String {
        return "Oi"
    }

    @Get("/api/fretes")
    fun calcula(@QueryValue cep: String): FreteResponse {
        try {
            val request = CalculaFreteRequest.newBuilder()
                .setCep(cep)
                .build()

            val response = gRpcClient.calculaFrete(request)

            return FreteResponse(
                cep = response.cep,
                valor = response.valor,
            )
        } catch (e: StatusRuntimeException) {
            val description = e.status.description
            when (e.status.code) {
                // verifica se é um erro e validação
                Status.Code.INVALID_ARGUMENT -> throw HttpStatusException(HttpStatus.BAD_REQUEST, description)
                // verifica se é um erro de autenticação
                Status.Code.PERMISSION_DENIED -> {
                    val statusProto =
                        StatusProto.fromThrowable(e) ?: throw HttpStatusException(HttpStatus.FORBIDDEN, description)

                    val anyDetails: Any = statusProto.detailsList[0]
                    val errorDetails = anyDetails.unpack(ErrorDetails::class.java)

                    throw HttpStatusException(HttpStatus.FORBIDDEN, "${errorDetails.code}: ${errorDetails.message}")
                }
                // caso contrário trata como um erro inesperado
                else -> throw HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.message) // code + description
            }
        }
    }

    data class FreteResponse(
        val cep: String,
        val valor: Double
    )
}