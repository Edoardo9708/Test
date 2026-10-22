// This adapter intentionally uses the internals of pinned libheif 1.23.6.
// It emits a standard auxl reference and Apple auxC, without changing vendor files.
#include "api_structs.h"
#include "context.h"
#include "file.h"
#include "image-items/image_item.h"
extern "C" heif_error codecprobe_assign_apple_gainmap(heif_context* context,const heif_image_handle* primary,const heif_image_handle* auxiliary){
    return exception_guard([&]() -> heif_error {
        if(!context||!primary||!auxiliary||primary==auxiliary||primary->context!=context->context||auxiliary->context!=context->context)return heif_error_null_pointer_argument;
        const std::string type="urn:com:apple:photo:2020:aux:hdrgainmap";
        auto file=context->context->get_heif_file();auto id=auxiliary->image->get_id();
        if(Error error=file->set_auxC_property(id,type))return error.error_struct(auxiliary->image.get());
        file->add_iref_reference(id,fourcc("auxl"),{primary->image->get_id()});
        file->get_infe_box(id)->set_hidden_item(true);
        auxiliary->image->set_is_aux_image(type);primary->image->add_aux_image(auxiliary->image);
        return heif_error_ok;
    });
}
