package lk.sasindi.luxeflora.api;

import lk.sasindi.luxeflora.model.UploadResponse;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface CloudinaryApi {

    @Multipart
    @POST("image/upload")
    Call<UploadResponse> uploadImage(
            @Part MultipartBody.Part file,
            @Part("upload_preset") RequestBody preset
    );
}
